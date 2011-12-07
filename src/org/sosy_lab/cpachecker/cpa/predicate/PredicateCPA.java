/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ContinuousStatistics;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsConsumer;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.util.clustering.CCVisuClusterer;
import org.sosy_lab.cpachecker.util.clustering.ClusteringStatistics;
import org.sosy_lab.cpachecker.util.clustering.interfaces.Clusterer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * CPA that defines symbolic predicate abstraction.
 */
@Options(prefix="cpa.predicate")
public class PredicateCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCPA.class).withOptions(BlockOperator.class);
  }

  @Option(name="abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"},
      description="which solver to use?")
  private String whichProver = "MATHSAT";

  @Option(name="abstraction.initialPredicates", type=Option.Type.OPTIONAL_INPUT_FILE,
      description="get an initial set of predicates from a file in MSAT format")
  private File predicatesFile = null;

  @Option(description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  @Option(name="blk.useCache", description="use caching of path formulas")
  private boolean useCache = true;

  @Option(name="merge", values={"SEP", "ABE"}, toUppercase=true,
      description="which merge operator to use for predicate cpa (usually ABE should be used)")
  private String mergeType = "ABE";

  private final Configuration config;
  private final LogManager logger;

  private final PredicateAbstractDomain domain;
  private final PredicateTransferRelation transfer;
  private final MergeOperator merge;
  private final PredicatePrecisionAdjustment prec;
  private final StopOperator stop;
  private final PredicatePrecision initialPrecision;
  private final RegionManager regionManager;
  private final ExtendedFormulaManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final TheoremProver theoremProver;
  private final PredicateAbstractionManager predicateManager;
  private final PredicateCPAStatistics stats;
  private final AbstractElement topElement;
  private final ClusteringStatistics clusteringStats;

  protected PredicateCPA(Configuration pConfig, LogManager pLogger, BlockOperator pBlk, CFA pCfa) throws InvalidConfigurationException {
    pConfig.inject(this, PredicateCPA.class);

    this.config = pConfig;
    this.logger = pLogger;

    // Inject the CFA into the BlockOperator.
    // TODO: This should be done with the "automatic" injection technique used by CPAchecker.
    this.clusteringStats = new ClusteringStatistics();

    Clusterer clusterer = new CCVisuClusterer(pConfig, pLogger, clusteringStats);
    clusterer.buildClustering(pCfa);
    pBlk.setCfa(pCfa);
    pBlk.setClusterer(clusterer);

    regionManager = BDDRegionManager.getInstance();
    MathsatFormulaManager mathsatFormulaManager = MathsatFactory.createFormulaManager(pConfig, pLogger);
    formulaManager = new ExtendedFormulaManager(mathsatFormulaManager, pConfig, pLogger);

    PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, pConfig, pLogger);
    if (useCache) {
      pfMgr = new CachingPathFormulaManager(pfMgr);
    }
    pathFormulaManager = pfMgr;

    if (whichProver.equals("MATHSAT")) {
      theoremProver = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      theoremProver = new YicesTheoremProver(formulaManager, pLogger);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    predicateManager = new PredicateAbstractionManager(regionManager, formulaManager, theoremProver, pConfig, pLogger);
    transfer = new PredicateTransferRelation(this, pBlk);

    topElement = PredicateAbstractElement.abstractionElement(pathFormulaManager.makeEmptyPathFormula(), predicateManager.makeTrueAbstractionFormula(null));
    domain = new PredicateAbstractDomain(this);

    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("ABE")) {
      merge = new PredicateMergeOperator(this);
    } else {
      throw new InternalError("Update list of allowed merge operators");
    }

    prec = new PredicatePrecisionAdjustment(this);

    stop = new StopSepOperator(domain);

    Collection<AbstractionPredicate> predicates = readPredicatesFromFile();

    if (checkBlockFeasibility) {
      AbstractionPredicate p = predicateManager.makeFalsePredicate();
      if (predicates == null) {
        predicates = ImmutableSet.of(p);
      } else {
        predicates.add(p);
      }
    }
    initialPrecision = new PredicatePrecision(predicates);

    stats = new PredicateCPAStatistics(this, pBlk);
  }

  private Collection<AbstractionPredicate> readPredicatesFromFile() {
    if (predicatesFile != null) {
        try {
        String fileContent = Files.toString(predicatesFile, Charset.defaultCharset());
        Formula f = formulaManager.parse(fileContent);

        Collection<Formula> atoms = formulaManager.extractAtoms(f, false, false);

        Collection<AbstractionPredicate> predicates = new ArrayList<AbstractionPredicate>(atoms.size());

        for (Formula atom : atoms) {
          predicates.add(predicateManager.makePredicate(atom));
        }
        return predicates;

      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicates from file " + predicatesFile);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicates from file");
      }
    }

    return null;
  }

  @Override
  public PredicateAbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public PredicateTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  RegionManager getRegionManager() {
    return regionManager;
  }

  PredicateAbstractionManager getPredicateManager() {
    return predicateManager;
  }

  public ExtendedFormulaManager getFormulaManager() {
    return formulaManager;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }

  public TheoremProver getTheoremProver() {
    return theoremProver;
  }

  Configuration getConfiguration() {
    return config;
  }

  LogManager getLogger() {
    return logger;
  }

  @Override
  public AbstractElement getInitialElement(CFANode node) {
    return topElement;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return initialPrecision;
  }

  @Override
  public PredicatePrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public void collectStatistics(StatisticsConsumer statsConsumer) {
    statsConsumer.addTerminationStatistics(new Statistics[]{stats, clusteringStats});
    statsConsumer.addContinuousStatistics(new ContinuousStatistics[]{stats});
  }

  PredicateCPAStatistics getStats() {
    return stats;
  }
}

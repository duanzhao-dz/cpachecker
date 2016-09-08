/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PartitionBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting.PropertyBudgeting;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning.PartitioningStatus;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Options
abstract class AbstractPartitioningOperator implements PartitioningOperator {

  protected final Configuration config;
  protected final LogManager logger;

  public AbstractPartitioningOperator(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    config = Preconditions.checkNotNull(pConfig);
    logger = Preconditions.checkNotNull(pLogger);

    pConfig.inject(this);
  }

  /**
   *
   * @return  Either the bisect partitioning,
   *          or the same partitioning as before
   *            (if all partitions consist already of one element)
   */
  @VisibleForTesting
  protected ImmutableList<ImmutableSet<Property>> bisectPartitons(ImmutableList<ImmutableSet<Property>> pInput,
      Comparator<Property> pComp) {

    ImmutableList.Builder<ImmutableSet<Property>> result = ImmutableList.<ImmutableSet<Property>>builder();

    for (final ImmutableSet<Property> p: pInput) {
      final ArrayList<Property> l = Lists.newArrayList(p);
      Collections.sort(l, pComp);

      if (l.size() > 1) {

        int firstElementsToAdd = (int) Math.ceil(l.size() / 2.0);

        result.add(ImmutableSet.<Property>copyOf(l.subList(0, firstElementsToAdd)));
        result.add(ImmutableSet.<Property>copyOf(l.subList(firstElementsToAdd, l.size())));

      } else {
        result.add(p);
      }
    }

    return result.build();
  }

  @VisibleForTesting
  protected ImmutableList<ImmutableSet<Property>> singletonPartitions(Set<Property> pInput,
      Comparator<Property> pComp) {

    final ImmutableList.Builder<ImmutableSet<Property>> result = ImmutableList.<ImmutableSet<Property>>builder();

    final ArrayList<Property> l = Lists.newArrayList(pInput);
    Collections.sort(l, pComp);

    for (Property p: l) {
      result.add(ImmutableSet.of(p));
    }

    return result.build();
  }

  protected ImmutableList<ImmutableSet<Property>> singletonPartitions(ImmutableSortedSet<Property> pInput) {

    final ImmutableList.Builder<ImmutableSet<Property>> result = ImmutableList.<ImmutableSet<Property>>builder();

    for (Property p: pInput) {
      result.add(ImmutableSet.of(p));
    }

    return result.build();
  }

  @VisibleForTesting
  protected ImmutableList<ImmutableSet<Property>> partitionsWithAtMost(int pK, Set<Property> pInput, Comparator<Property> pComp) {
    if (pK <= 0) {
      return ImmutableList.of(ImmutableSet.copyOf(pInput));
    }

    final ImmutableList.Builder<ImmutableSet<Property>> result = ImmutableList.<ImmutableSet<Property>>builder();

    final ArrayList<Property> l = Lists.newArrayList(pInput);
    Collections.sort(l, pComp);

    int inPartition = 0;
    Iterator<Property> it = l.iterator();
    Set<Property> active = Sets.newLinkedHashSet();

    while (it.hasNext()) {
      Property p = it.next();
      if (inPartition >= pK) {
        result.add(ImmutableSet.copyOf(active));
        active = Sets.newLinkedHashSet();
        inPartition = 0;
      }
      inPartition++;
      active.add(p);
    }

    if (active.size() > 0) {
      result.add(ImmutableSet.copyOf(active));
    }

    return result.build();
  }

  protected Partitioning create(final PartitioningStatus pStatus,
      PropertyBudgeting pPropertyBudgeting,
      PartitionBudgeting pPartitionBudgeting,
      final ImmutableList<ImmutableSet<Property>> pPartitions) {

    return Partitions.partitions(pStatus, pPropertyBudgeting, pPartitionBudgeting, pPartitions);
  }

  protected ImmutableList<ImmutableSet<Property>> immutable(List<Set<Property>> pInput) {
    Builder<ImmutableSet<Property>> result = ImmutableList.<ImmutableSet<Property>>builder();
    for (Set<Property> partition: pInput) {
      result.add(ImmutableSet.copyOf(partition));
    }
    return result.build();
  }

}
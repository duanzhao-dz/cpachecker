/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.ProverEnvironment;


/**
 * Class for creating {@link Formula}s out of {@link Constraint}s
 */
public interface FormulaCreator {

  /**
   * Creates a {@link BooleanFormula} representing the boolean meaning of the given
   * {@link SymbolicValue symbolic value}.
   *
   * <p>
   * Examples:<br />
   * <ul>
   * <li>Symbolic value <tt>s1</tt> is transformed to <tt>s1 != 0</tt> with type
   * <code>BooleanFormula</code></li>
   * <li>Symbolic value <tt>s1 > 0</tt> is transformed to <tt>s1 > 0</tt> (meaning stays the
   * same, since it already is a boolean expression) with type <code>BooleanFormula</code></li>
   * </ul>
   * </p>
   *
   * @param pValue the symbolic value to create a formula of
   * @return a <code>Formula</code> representing the given constraint
   * @see #createPredicate(SymbolicValue, IdentifierAssignment)
   */
  BooleanFormula createPredicate(SymbolicValue pValue) throws UnrecognizedCCodeException,
                                                              InterruptedException;

  /**
   * Creates a {@link BooleanFormula} representing the boolean meaning of the given
   * {@link SymbolicValue symbolic value}.
   * Symbolic Identifiers in constraints are replaced by their known definite assignments, if
   * one exists.
   *
   * <p>
   * Examples:<br />
   * <ul>
   * <li>Symbolic value <tt>s1</tt> is transformed to <tt>s1 != 0</tt> with type
   * <code>BooleanFormula</code></li>
   * <li>Symbolic value <tt>s1 > 0</tt> is transformed to <tt>s1 > 0</tt> (meaning stays the
   * same, since it already is a boolean expression) with type <code>BooleanFormula</code></li>
   * </ul>
   * </p>
   *
   * @param pValue the symbolic value to create a boolean formula of
   * @param pDefiniteAssignment the known definite assignments of symbolic identifiers
   *
   * @return a <code>BooleanFormula</code> representing the boolean meaning of the given symbolic
   *    value
   * @see #createPredicate(SymbolicValue)
   */
  BooleanFormula createPredicate(SymbolicValue pValue, IdentifierAssignment pDefiniteAssignment)
      throws UnrecognizedCCodeException, InterruptedException;


  /**
   * Creates a {@link Formula} representing the given {@link SymbolicValue}.
   *
   * @param pValue the symbolic value to create a formula of
   * @return a <code>Formula</code> representing the given symbolic value
   */
  Formula createTerm(SymbolicValue pValue) throws UnrecognizedCCodeException;

  /**
   * Creates a {@link Formula} representing the given {@link SymbolicValue}.
   *
   * @param pValue the symbolic value to create a formula of
   * @param pDefiniteAssignment the known definite assignments of symbolic identifiers
   * @return a <code>Formula</code> representing the given symbolic value
   */
  Formula createTerm(SymbolicValue pValue, IdentifierAssignment pDefiniteAssignment)
      throws UnrecognizedCCodeException;

  /**
   * Creates a {@link BooleanFormula} representing the given term-value assignment.
   *
   * <p>These assignments are usually returned by {@link ProverEnvironment#getModel()} after a
   * successful SAT check.</p>
   *
   * <p>Example: Given variable <code>a</code> and <code>5</code>, this method
   * returns the formula <code>a equals 5</code>
   * </p>
   *
   * @param pVariable the variable as a formula to assign the given value to
   * @param pTermAssignment the value of the assignment
   * @return a <code>BooleanFormula</code> representing the given assignment
   */
  BooleanFormula transformAssignment(Formula pVariable, Object pTermAssignment);
}

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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

/**
 * Interface that represents the final results of a CPAchecker algorithm.
 */
public interface AlgorithmResult {


  public static class CounterexampleInfoResult implements AlgorithmResult {
    private final Optional<CounterexampleInfo> counterexampleInfo;

    public CounterexampleInfoResult(Optional<CounterexampleInfo> pCounterexampleInfo) {
      Preconditions.checkNotNull(pCounterexampleInfo);
      counterexampleInfo = pCounterexampleInfo;
    }

    public Optional<CounterexampleInfo> getCounterexampleInfo() {
      return counterexampleInfo;
    }
  }
}
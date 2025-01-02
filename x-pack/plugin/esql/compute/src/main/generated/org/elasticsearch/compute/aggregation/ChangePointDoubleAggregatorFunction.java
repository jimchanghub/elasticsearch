// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.BooleanVector;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.DoubleVector;
import org.elasticsearch.compute.data.ElementType;
import org.elasticsearch.compute.data.LongBlock;
import org.elasticsearch.compute.data.LongVector;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunction} implementation for {@link ChangePointDoubleAggregator}.
 * This class is generated. Do not edit it.
 */
public final class ChangePointDoubleAggregatorFunction implements AggregatorFunction {
  private static final List<IntermediateStateDesc> INTERMEDIATE_STATE_DESC = List.of(
      new IntermediateStateDesc("timestamps", ElementType.LONG),
      new IntermediateStateDesc("values", ElementType.DOUBLE)  );

  private final DriverContext driverContext;

  private final ChangePointStates.SingleState state;

  private final List<Integer> channels;

  public ChangePointDoubleAggregatorFunction(DriverContext driverContext, List<Integer> channels,
      ChangePointStates.SingleState state) {
    this.driverContext = driverContext;
    this.channels = channels;
    this.state = state;
  }

  public static ChangePointDoubleAggregatorFunction create(DriverContext driverContext,
      List<Integer> channels) {
    return new ChangePointDoubleAggregatorFunction(driverContext, channels, ChangePointDoubleAggregator.initSingle(driverContext));
  }

  public static List<IntermediateStateDesc> intermediateStateDesc() {
    return INTERMEDIATE_STATE_DESC;
  }

  @Override
  public int intermediateBlockCount() {
    return INTERMEDIATE_STATE_DESC.size();
  }

  @Override
  public void addRawInput(Page page, BooleanVector mask) {
    if (mask.allFalse()) {
      // Entire page masked away
      return;
    }
    DoubleBlock block = page.getBlock(channels.get(0));
    DoubleVector vector = block.asVector();
    LongBlock timestampsBlock = page.getBlock(channels.get(1));
    LongVector timestampsVector = timestampsBlock.asVector();
    if (timestampsVector == null)  {
      throw new IllegalStateException("expected @timestamp vector; but got a block");
    }
    if (mask.allTrue()) {
      // No masking
      if (vector != null) {
        addRawVector(vector, timestampsVector);
      } else {
        addRawBlock(block, timestampsVector);
      }
      return;
    } else {
      // Some positions masked away, others kept
      if (vector != null) {
        addRawVector(vector, timestampsVector, mask);
      } else {
        addRawBlock(block, timestampsVector, mask);
      }
    }
  }

  private void addRawVector(DoubleVector vector, LongVector timestamps) {
    for (int i = 0; i < vector.getPositionCount(); i++) {
      ChangePointDoubleAggregator.combine(state, timestamps.getLong(i), vector.getDouble(i));
    }
  }

  private void addRawVector(DoubleVector vector, LongVector timestamps, BooleanVector mask) {
    for (int i = 0; i < vector.getPositionCount(); i++) {
      if (mask.getBoolean(i) == false) {
        continue;
      }
      ChangePointDoubleAggregator.combine(state, timestamps.getLong(i), vector.getDouble(i));
    }
  }

  private void addRawBlock(DoubleBlock block, LongVector timestamps) {
    for (int p = 0; p < block.getPositionCount(); p++) {
      if (block.isNull(p)) {
        continue;
      }
      int start = block.getFirstValueIndex(p);
      int end = start + block.getValueCount(p);
      for (int i = start; i < end; i++) {
        ChangePointDoubleAggregator.combine(state, timestamps.getLong(i), block.getDouble(i));
      }
    }
  }

  private void addRawBlock(DoubleBlock block, LongVector timestamps, BooleanVector mask) {
    for (int p = 0; p < block.getPositionCount(); p++) {
      if (mask.getBoolean(p) == false) {
        continue;
      }
      if (block.isNull(p)) {
        continue;
      }
      int start = block.getFirstValueIndex(p);
      int end = start + block.getValueCount(p);
      for (int i = start; i < end; i++) {
        ChangePointDoubleAggregator.combine(state, timestamps.getLong(i), block.getDouble(i));
      }
    }
  }

  @Override
  public void addIntermediateInput(Page page) {
    assert channels.size() == intermediateBlockCount();
    assert page.getBlockCount() >= channels.get(0) + intermediateStateDesc().size();
    Block timestampsUncast = page.getBlock(channels.get(0));
    if (timestampsUncast.areAllValuesNull()) {
      return;
    }
    LongBlock timestamps = (LongBlock) timestampsUncast;
    assert timestamps.getPositionCount() == 1;
    Block valuesUncast = page.getBlock(channels.get(1));
    if (valuesUncast.areAllValuesNull()) {
      return;
    }
    DoubleBlock values = (DoubleBlock) valuesUncast;
    assert values.getPositionCount() == 1;
    ChangePointDoubleAggregator.combineIntermediate(state, timestamps, values);
  }

  @Override
  public void evaluateIntermediate(Block[] blocks, int offset, DriverContext driverContext) {
    state.toIntermediate(blocks, offset, driverContext);
  }

  @Override
  public void evaluateFinal(Block[] blocks, int offset, DriverContext driverContext) {
    blocks[offset] = ChangePointDoubleAggregator.evaluateFinal(state, driverContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");
    sb.append("channels=").append(channels);
    sb.append("]");
    return sb.toString();
  }

  @Override
  public void close() {
    state.close();
  }
}

package com.autonomouslogic.everef.cli.marketorders;

/**
 * Lightweight sort key for a market order, kept in memory so the full order (held in an MVStore)
 * doesn't need to be deserialized while sorting.
 */
public record OrderSortKey(Long regionId, Long typeId, Long isBuyOrder, Long systemId, long orderId) {}

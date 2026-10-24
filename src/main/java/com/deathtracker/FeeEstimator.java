package com.deathtracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A standard-PvM gravestone approximation, not a substitute for the game's death interface. */
final class FeeEstimator
{
    private FeeEstimator() {}

    static long estimate(List<TrackedItem> equipment, List<TrackedItem> inventory,
        boolean skulled, boolean protectItem, boolean ironman)
    {
        List<TrackedItem> priced = new ArrayList<>();
        priced.addAll(equipment);
        priced.addAll(inventory);
        priced.sort(Comparator.comparingLong(TrackedItem::value).reversed());

        int protectedSlots = (skulled ? 0 : 3) + (protectItem ? 1 : 0);
        long fee = 0;
        for (int i = protectedSlots; i < priced.size(); i++)
        {
            TrackedItem item = priced.get(i);
            if (!item.tradeable)
            {
                continue;
            }
            long value = item.unitPrice;
            long eachFee = value >= 10_000_000 ? 100_000 :
                value >= 1_000_000 ? 10_000 : value >= 100_000 ? 1_000 : 0;
            // Stack size is deliberately excluded: the ordinary grave fee is per item stack.
            fee += eachFee;
        }
        fee = Math.min(fee, 500_000);
        return ironman ? fee / 2 : fee;
    }
}

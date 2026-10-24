package com.deathtracker;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class FeeEstimatorTest
{
    private static TrackedItem item(long price)
    {
        TrackedItem item = new TrackedItem();
        item.unitPrice = price;
        item.quantity = 1;
        item.tradeable = true;
        return item;
    }

    @Test
    public void usesProtectedSlotsAndIronmanDiscount()
    {
        Assert.assertEquals(110_000, FeeEstimator.estimate(
            Arrays.asList(item(20_000_000), item(15_000_000), item(12_000_000)),
            Arrays.asList(item(11_000_000), item(2_000_000)), false, false, false));
        Assert.assertEquals(5_000, FeeEstimator.estimate(
            Arrays.asList(item(20_000_000), item(15_000_000), item(12_000_000)),
            Arrays.asList(item(11_000_000), item(2_000_000)), false, true, true));
    }

    @Test
    public void capsGraveFees()
    {
        Assert.assertEquals(500_000, FeeEstimator.estimate(
            Collections.nCopies(10, item(20_000_000)), Collections.emptyList(), true, false, false));
    }

    @Test
    public void onlyAcceptsConfirmedReclaimPayments()
    {
        Assert.assertEquals(Long.valueOf(42_000), ReclaimMessageParser.actualFee("You paid 42,000 coins to reclaim your items from the gravestone."));
        Assert.assertNull(ReclaimMessageParser.actualFee("Death says reclaiming your items will cost 42,000 coins."));
        Assert.assertNull(ReclaimMessageParser.actualFee("You paid 42,000 coins to buy a sword."));
    }
}

package com.deathtracker;

import java.util.ArrayList;
import java.util.List;

/** A durable snapshot; values are fixed at the moment the death was recorded. */
public class DeathRecord
{
    public String id;
    public long timestamp;
    public String player;
    public int world;
    public int x;
    public int y;
    public int plane;
    public int region;
    public boolean instance;
    public String opponent;
    public String accountType;
    public boolean skulled;
    public boolean protectItem;
    public List<TrackedItem> equipment = new ArrayList<>();
    public List<TrackedItem> inventory = new ArrayList<>();
    public long carriedValue;
    public Long estimatedGraveFee;
    public String estimateNote;
    public String screenshot;
    public Long actualReclaimFee;
    public Long actualItemLoss;
    public Long actualOtherCost;
    public String reclaimMethod;
    public String notes;

    public Long actualTotal()
    {
        if (actualReclaimFee == null && actualItemLoss == null && actualOtherCost == null)
        {
            return null;
        }
        return zero(actualReclaimFee) + zero(actualItemLoss) + zero(actualOtherCost);
    }

    private static long zero(Long value)
    {
        return value == null ? 0L : value;
    }
}

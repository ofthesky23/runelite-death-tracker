package com.deathtracker;

public class TrackedItem
{
    public int slot;
    public int id;
    public int quantity;
    public String name;
    public long unitPrice;
    public boolean tradeable;

    public long value()
    {
        return unitPrice * (long) quantity;
    }
}

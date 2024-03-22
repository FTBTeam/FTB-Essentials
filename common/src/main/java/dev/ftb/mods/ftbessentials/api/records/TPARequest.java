package dev.ftb.mods.ftbessentials.api.records;

import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;

import java.util.UUID;

public record TPARequest(
        UUID id,
        FTBEPlayerData source,
        FTBEPlayerData target,
        boolean here,
        long created
) {}

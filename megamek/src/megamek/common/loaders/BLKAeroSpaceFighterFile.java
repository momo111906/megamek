/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2019 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.common.loaders;

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import megamek.common.verifier.TestEntity;

/**
 * BLkFile.java
 * <p>
 * Created on April 6, 2002, 2:06 AM
 *
 * @author taharqa
 */
public class BLKAeroSpaceFighterFile extends BLKFile implements IMekLoader {

    // armor locations
    public static final int NOSE = 0;
    public static final int RW = 1;
    public static final int LW = 2;
    public static final int AFT = 3;

    public BLKAeroSpaceFighterFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        AeroSpaceFighter a = new AeroSpaceFighter();

        setBasicEntityData(a);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        // get a movement mode - lets try Aerodyne
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        a.setMovementMode(nMotion);

        a.setVSTOL(true);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heatsinks block.");
        }
        a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        a.setOHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        if (dataFile.exists("omnipodheatsinks")) {
            a.setPodHeatSinks(dataFile.getDataAsInt("omnipodheatsinks")[0]);
        }
        if (!dataFile.exists("sink_type")) {
            throw new EntityLoadingException("Could not find sink_type block.");
        }
        a.setHeatType(dataFile.getDataAsInt("sink_type")[0]);

        // figure out fuel
        if (!dataFile.exists("fuel")) {
            throw new EntityLoadingException("Could not find fuel block.");
        }
        a.setFuel(dataFile.getDataAsInt("fuel")[0]);

        // figure out engine stuff
        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        int engineFlags = 0;
        // Support for mixed tech units with an engine with a different tech base
        if (dataFile.exists("clan_engine")) {
            if (Boolean.parseBoolean(dataFile.getDataAsString("clan_engine")[0])) {
                engineFlags |= Engine.CLAN_ENGINE;
            }
        } else if (a.isClan()) {
            engineFlags |= Engine.CLAN_ENGINE;
        }
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find SafeThrust block.");
        }

        // set cockpit type if not default
        if (dataFile.exists("cockpit_type")) {
            a.setCockpitType(dataFile.getDataAsInt("cockpit_type")[0]);
        }

        int engineRating = (dataFile.getDataAsInt("SafeThrust")[0] - 2) * (int) a.getWeight();
        if (a.isPrimitive()) {
            engineRating = Math.round(engineRating * 1.2f);

            // Ensure the rating is divisible by 5
            if ((engineRating % 5) != 0) {
                engineRating = engineRating - (engineRating % 5) + 5;
            }
        }
        a.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(engineCode), engineFlags));

        boolean patchworkArmor = false;
        if (dataFile.exists("armor_type")) {
            if (dataFile.getDataAsInt("armor_type")[0] == EquipmentType.T_ARMOR_PATCHWORK) {
                patchworkArmor = true;
            } else {
                a.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
            }
        } else {
            a.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        }
        if (!patchworkArmor && dataFile.exists("armor_tech")) {
            a.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (patchworkArmor) {
            for (int i = 0; i < (a.locations() - 1); i++) {
                a.setArmorType(dataFile.getDataAsInt(a.getLocationName(i) + "_armor_type")[0], i);
                a.setArmorTechLevel(dataFile.getDataAsInt(a.getLocationName(i) + "_armor_type")[0], i);
            }
        }

        if (dataFile.exists("internal_type")) {
            a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            a.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 4) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        a.initializeArmor(armor[BLKAeroSpaceFighterFile.NOSE], Aero.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.RW], Aero.LOC_RWING);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.LW], Aero.LOC_LWING);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.AFT], Aero.LOC_AFT);
        a.initializeArmor(0, Aero.LOC_WINGS);
        a.initializeArmor(IArmorState.ARMOR_NA, Aero.LOC_FUSELAGE);

        a.autoSetCapArmor();
        a.autoSetFatalThresh();

        a.autoSetInternal();
        a.recalculateTechAdvancement();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        // add Transporters prior to equipment to simplify F_CARGO bay assignment
        addTransports(a);

        for (int loc = 0; loc < a.locations(); loc++) {
            loadEquipment(a, a.getLocationName(loc), loc);
        }

        // now organize the weapons into groups for capital fighters
        a.updateWeaponGroups();

        if (dataFile.exists("omni")) {
            a.setOmni(true);
        }

        a.setArmorTonnage(a.getArmorWeight());
        loadQuirks(a);
        return a;
    }

    @Override
    protected void loadEquipment(Entity t, String sName, int nLoc) throws EntityLoadingException {
        boolean addedCase = false;
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) {
            return;
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        boolean rearMount;

        if (saEquip[0] != null) {
            for (String element : saEquip) {
                rearMount = false;
                String equipName = element.trim();

                boolean omniMounted = equipName.contains(":OMNI");
                equipName = equipName.replace(":OMNI", "");

                double size = 0.0;
                int sizeIndex = equipName.toUpperCase().indexOf(":SIZE:");
                if (sizeIndex > 0) {
                    size = Double.parseDouble(equipName.substring(sizeIndex + 6));
                    equipName = equipName.substring(0, sizeIndex);
                }
                if (equipName.startsWith("(R) ")) {
                    rearMount = true;
                    equipName = equipName.substring(4);
                }
                int facing = -1;
                if (equipName.toUpperCase().endsWith("(FL)")) {
                    facing = 5;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(FR)")) {
                    facing = 1;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(RL)")) {
                    facing = 4;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(RR)")) {
                    facing = 2;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }

                EquipmentType etype = EquipmentType.get(equipName);

                if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_CASE)) {
                    if (etype.isClan() || addedCase) {
                        continue;
                    }
                    addedCase = true;
                }

                // The stealth armor mount is added when the armor type is set
                if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_STEALTH)) {
                    continue;
                }

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    try {
                        int useLoc = TestEntity.eqRequiresLocation(t, etype) ? nLoc : Aero.LOC_FUSELAGE;
                        Mounted<?> mount = t.addEquipment(etype, useLoc, rearMount);
                        mount.setOmniPodMounted(omniMounted);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_VGL)) {
                            if (facing == -1) {
                                mount.setFacing(defaultAeroVGLFacing(useLoc, rearMount));
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                        if (etype.isVariableSize()) {
                            if (size == 0) {
                                size = MtfFile.extractLegacySize(equipName);
                            }
                            mount.setSize(size);
                        }
                        if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_CARGO)) {
                            // Treat F_CARGO equipment as cargo bays with 1 door, e.g. for ASF with IBB.
                            int idx = t.getTransportBays().size();
                            double baySize = (mount.getName().contains("Container")) ?
                                                   mount.getTonnage() :
                                                   mount.getSize();
                            t.addTransporter(new CargoBay(baySize, 1, idx), omniMounted);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (!equipName.isBlank()) {
                    t.addFailedEquipment(equipName);
                }
            }
        }
    }
}

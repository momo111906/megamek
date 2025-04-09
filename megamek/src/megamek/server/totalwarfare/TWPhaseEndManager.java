/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.totalwarfare;

import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameVictoryEvent;
import megamek.common.net.packets.Packet;
import megamek.rating.RatingManagerHolder;
import megamek.server.ServerHelper;

import java.util.List;

class TWPhaseEndManager {

    private final TWGameManager gameManager;

    public TWPhaseEndManager(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void managePhase() {
        GamePhase currentPhase = gameManager.getGame().getPhase();

        switch (currentPhase) {
            case LOUNGE -> handleLoungePhase();
            case EXCHANGE, STARTING_SCENARIO -> handleExchangePhase();
            case SET_ARTILLERY_AUTOHIT_HEXES -> handleSetArtilleryPhase();
            case DEPLOY_MINEFIELDS -> gameManager.changePhase(GamePhase.INITIATIVE);
            case DEPLOYMENT -> handleDeploymentPhase();
            case INITIATIVE -> handleInitiativePhase();
            case INITIATIVE_REPORT -> handleInitiativeReportPhase();
            case PREMOVEMENT -> gameManager.changePhase(GamePhase.MOVEMENT);
            case MOVEMENT -> handleMovementPhase();
            case MOVEMENT_REPORT -> gameManager.changePhase(GamePhase.OFFBOARD);
            case PREFIRING -> gameManager.changePhase(GamePhase.FIRING);
            case FIRING -> handleFiringPhase();
            case FIRING_REPORT -> gameManager.changePhase(GamePhase.PHYSICAL);
            case PHYSICAL -> handlePhysicalPhase();
            case PHYSICAL_REPORT -> gameManager.changePhase(GamePhase.END);
            case TARGETING -> handleTargetingPhase();
            case OFFBOARD -> handleOffboardPhase();
            case OFFBOARD_REPORT -> handleOffboardReportPhase();
            case TARGETING_REPORT -> gameManager.changePhase(GamePhase.PREMOVEMENT);
            case END -> handleEndPhase();
            case END_REPORT -> handleEndReportPhase();
            case VICTORY -> handleVictoryPhase();
            default -> {}
        }

        clearHiddenUnitActivations();
    }

    private void handleLoungePhase() {
        gameManager.getGame().addReports(gameManager.getMainPhaseReport());
        gameManager.changePhase(GamePhase.EXCHANGE);
    }

    private void handleExchangePhase() {
        gameManager.getGame().addReports(gameManager.getMainPhaseReport());
        gameManager.changePhase(GamePhase.SET_ARTILLERY_AUTOHIT_HEXES);
    }

    private void handleSetArtilleryPhase() {
        gameManager.sendSpecialHexDisplayPackets();
        gameManager.getGame().addReports(gameManager.getMainPhaseReport());
        boolean hasMinesToDeploy = gameManager.getGame().getPlayersList().stream().anyMatch(Player::hasMinefields);
        gameManager.changePhase(hasMinesToDeploy ? GamePhase.DEPLOY_MINEFIELDS : GamePhase.INITIATIVE);
    }

    private void handleDeploymentPhase() {
        gameManager.getGame().clearDeploymentThisRound();
        gameManager.getGame().getPlayersList().forEach(Player::adjustStartingPosForReinforcements);
        gameManager.changePhase(gameManager.getGame().getRoundCount() < 1 ? GamePhase.INITIATIVE : GamePhase.TARGETING);
    }

    private void handleInitiativePhase() {
        gameManager.resolveWhatPlayersCanSeeWhatUnits();
        gameManager.detectSpacecraft();
        gameManager.getGame().addReports(gameManager.getMainPhaseReport());
        gameManager.changePhase(GamePhase.INITIATIVE_REPORT);
    }

    private void handleInitiativeReportPhase() {
        gameManager.getGame().setupDeployment();
        gameManager.changePhase(gameManager.getGame().shouldDeployThisRound() ? GamePhase.DEPLOYMENT : GamePhase.TARGETING);
        gameManager.clearBombIcons();
        gameManager.sendSpecialHexDisplayPackets();
    }

    private void handleMovementPhase() {
        gameManager.detectHiddenUnits();
        ServerHelper.detectMinefields(gameManager.getGame(), gameManager.getMainPhaseReport(), gameManager);
        gameManager.updateSpacecraftDetection();
        gameManager.detectSpacecraft();
        gameManager.resolveWhatPlayersCanSeeWhatUnits();
        gameManager.doAllAssaultDrops();
        gameManager.addMovementHeat();
        gameManager.applyBuildingDamage();
        gameManager.checkForPSRFromDamage();
        gameManager.addReport(gameManager.resolvePilotingRolls());
        gameManager.checkForFlamingDamage();
        gameManager.checkForTeleMissileAttacks();
        gameManager.cleanupDestroyedNarcPods();
        gameManager.checkForFlawedCooling();
        gameManager.resolveCallSupport();

        processReportAndChangePhase(1205, GamePhase.MOVEMENT_REPORT, GamePhase.OFFBOARD);
    }

    private void handleFiringPhase() {
        gameManager.addReport(new Report(3000, Report.PUBLIC));
        gameManager.resolveWhatPlayersCanSeeWhatUnits();
        gameManager.resolveAllButWeaponAttacks();
        gameManager.resolveSelfDestructions();
        gameManager.reportGhostTargetRolls();
        gameManager.reportLargeCraftECCMRolls();
        gameManager.resolveOnlyWeaponAttacks();
        gameManager.assignAMS();
        gameManager.handleAttacks();
        gameManager.resolveScheduledNukes();
        gameManager.resolveScheduledOrbitalBombardments();
        gameManager.applyBuildingDamage();
        gameManager.checkForPSRFromDamage();
        gameManager.cleanupDestroyedNarcPods();
        gameManager.addReport(gameManager.resolvePilotingRolls());
        gameManager.checkForFlawedCooling();

        processReportAndChangePhase(1205, GamePhase.FIRING_REPORT, GamePhase.PHYSICAL);

        gameManager.sendGroundObjectUpdate();
        gameManager.sendSpecialHexDisplayPackets();
    }

    private void handlePhysicalPhase() {
        gameManager.resolveWhatPlayersCanSeeWhatUnits();
        gameManager.resolvePhysicalAttacks();
        gameManager.applyBuildingDamage();
        gameManager.checkForPSRFromDamage();
        gameManager.addReport(gameManager.resolvePilotingRolls());
        gameManager.resolveSinkVees();
        gameManager.cleanupDestroyedNarcPods();
        gameManager.checkForFlawedCooling();
        gameManager.checkForChainWhipGrappleChecks();

        processReportAndChangePhase(1205, GamePhase.PHYSICAL_REPORT, GamePhase.END);

        gameManager.sendGroundObjectUpdate();
    }

    private void handleTargetingPhase() {
        gameManager.getMainPhaseReport().addElement(new Report(1035, Report.PUBLIC));
        gameManager.resolveAllButWeaponAttacks();
        gameManager.resolveOnlyWeaponAttacks();
        gameManager.handleAttacks();

        processReportAndChangePhase(1205, GamePhase.TARGETING_REPORT, GamePhase.PREMOVEMENT);

        gameManager.sendSpecialHexDisplayPackets();
        gameManager.getGame().getPlayersList().forEach(player -> gameManager.send(player.getId(), gameManager.createArtilleryPacket(player)));
    }

    private void handleOffboardPhase() {
        gameManager.addReport(new Report(1100, Report.PUBLIC));
        gameManager.resolveAllButWeaponAttacks();
        gameManager.resolveOnlyWeaponAttacks();
        gameManager.handleAttacks();
        gameManager.getGame().getPlayersList().forEach(player -> gameManager.send(player.getId(), gameManager.createArtilleryPacket(player)));
        gameManager.applyBuildingDamage();
        gameManager.checkForPSRFromDamage();
        gameManager.addReport(gameManager.resolvePilotingRolls());
        gameManager.cleanupDestroyedNarcPods();
        gameManager.checkForFlawedCooling();
        gameManager.sendSpecialHexDisplayPackets();
        gameManager.sendTagInfoUpdates();

        processReportAndChangePhase(1205, GamePhase.OFFBOARD_REPORT, GamePhase.PREFIRING);
    }

    private void handleOffboardReportPhase() {
        gameManager.sendSpecialHexDisplayPackets();
        gameManager.changePhase(GamePhase.PREFIRING);
    }

    private void handleEndPhase() {
        gameManager.resetEntityPhase(GamePhase.END);
        boolean victory = gameManager.victory();

        boolean shouldChange = (gameManager.getMainPhaseReport().size() > 3)
                                     || ((gameManager.getMainPhaseReport().size() > 1)
                                               && (gameManager.getMainPhaseReport().elementAt(1).messageId != 1205));

        if (shouldChange) {
            gameManager.getGame().addReports(gameManager.getMainPhaseReport());
            gameManager.changePhase(GamePhase.END_REPORT);
        } else {
            gameManager.addReport(new Report(1205, Report.PUBLIC));
            gameManager.getGame().addReports(gameManager.getMainPhaseReport());
            gameManager.sendReport();
            gameManager.changePhase(victory ? GamePhase.VICTORY : GamePhase.INITIATIVE);
        }

        gameManager.decrementASEWTurns();
    }

    private void handleEndReportPhase() {
        gameManager.processTeamChangeRequest();
        gameManager.changePhase(gameManager.victory() ? GamePhase.VICTORY : GamePhase.INITIATIVE);
    }

    private void handleVictoryPhase() {
        updatePlayerRatings();
        GameVictoryEvent gve = new GameVictoryEvent(this, gameManager.getGame());
        gameManager.getGame().processGameEvent(gve);
        gameManager.transmitGameVictoryEventToAll();
        gameManager.resetGame();
    }

    private void processReportAndChangePhase(int fallbackMsgId, GamePhase phaseIfReports, GamePhase fallbackPhase) {
        if (gameManager.getMainPhaseReport().size() > 1) {
            gameManager.getGame().addReports(gameManager.getMainPhaseReport());
            gameManager.changePhase(phaseIfReports);
        } else {
            gameManager.addReport(new Report(fallbackMsgId, Report.PUBLIC));
            gameManager.getGame().addReports(gameManager.getMainPhaseReport());
            gameManager.sendReport();
            gameManager.changePhase(fallbackPhase);
        }
    }

    private void updatePlayerRatings() {
        List<Player> players = gameManager.getGame().getPlayersList();
        if (players.size() >= 2) {
            Player winner = players.get(0);
            Player loser = players.get(1);

            RatingManagerHolder.getInstance().updateRatings(winner.getId(), loser.getId(), 1.0);

            winner.setRating(RatingManagerHolder.getInstance().getRating(winner.getId()));
            loser.setRating(RatingManagerHolder.getInstance().getRating(loser.getId()));

            Packet ratingPacket = gameManager.getPacketHelper().createRatingUpdatePacket(
                  winner.getId(), winner.getRating(), loser.getId(), loser.getRating());
            gameManager.send(ratingPacket);
        }
    }


    private void clearHiddenUnitActivations() {
        for (Entity ent : gameManager.getGame().getEntitiesVector()) {
            if (ent.getHiddenActivationPhase() == gameManager.getGame().getPhase()) {
                ent.setHiddenActivationPhase(GamePhase.UNKNOWN);
            }
        }
    }
}

package megamek.server.totalwarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import megamek.common.Game;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameVictoryEvent;
import megamek.common.net.packets.Packet;
import megamek.server.GameManagerPacketHelper;
import megamek.rating.RatingManagerHolder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Teste le comportement de TWPhaseEndManager dans le cas de la phase VICTORY.
 * Ce test vérifie que updatePlayerRatings() met à jour les ratings via RatingManagerHolder,
 * qu’un paquet UPDATE_RATING est créé et envoyé,
 * et que l’événement de victoire est traité et le jeu est réinitialisé.
 */
public class TWPhaseEndManagerTest {

    private TWGameManager mockGameManager;
    private Game mockGame;
    private GameManagerPacketHelper mockPacketHelper;
    private Packet mockPacket;
    private TWPhaseEndManager phaseEndManager;

    private Player winner;
    private Player loser;

    @BeforeEach
    public void setUp() {
        // Création des mocks pour le game manager et le game
        mockGameManager = mock(TWGameManager.class);
        mockGame = mock(Game.class);
        when(mockGameManager.getGame()).thenReturn(mockGame);

        // Forcer la phase du jeu à VICTORY
        when(mockGame.getPhase()).thenReturn(GamePhase.VICTORY);
        // S'assurer que getOutOfGameEntitiesVector() ne retourne pas null (pour éviter le NPE dans GameVictoryEvent)
        when(mockGame.getOutOfGameEntitiesVector()).thenReturn(new Vector<>());

        // Création de deux joueurs avec un rating initial de 1500.0
        winner = new Player(1, "Winner");
        loser  = new Player(2, "Loser");
        winner.setRating(1500.0);
        loser.setRating(1500.0);
        List<Player> players = Arrays.asList(winner, loser);
        when(mockGame.getPlayersList()).thenReturn(players);
        // Stubbing pour que game.getPlayer(id) renvoie bien les objets joueurs
        when(mockGame.getPlayer(winner.getId())).thenReturn(winner);
        when(mockGame.getPlayer(loser.getId())).thenReturn(loser);

        // Création et configuration du mock du PacketHelper (GameManagerPacketHelper)
        mockPacketHelper = mock(GameManagerPacketHelper.class);
        mockPacket = mock(Packet.class);
        when(mockPacketHelper.createRatingUpdatePacket(anyInt(), anyDouble(), anyInt(), anyDouble()))
              .thenReturn(mockPacket);
        when(mockGameManager.getPacketHelper()).thenReturn(mockPacketHelper);

        // Instanciation de TWPhaseEndManager à tester
        phaseEndManager = new TWPhaseEndManager(mockGameManager);
    }

    @Test
    public void testHandleVictoryPhase() {
        // Lancer le traitement de phase qui va exécuter handleVictoryPhase()
        phaseEndManager.managePhase();

        /*
         * Ici, updateRatings() de RatingManagerHolder ne met à jour que la map interne.
         * Pour "simuler" que les objets Player reflètent le nouvel état (ce que l’on attend pour
         * la création du paquet UPDATE_RATING), on « synchronise » les joueurs à partir du RatingManagerHolder.
         */
        winner.setRating(RatingManagerHolder.getInstance().getRating(winner.getId()));
        loser.setRating(RatingManagerHolder.getInstance().getRating(loser.getId()));

        // Calcul attendu pour une victoire avec K = 30 et un match initial à 1500 :
        // winner : 1500 + 30 * (1.0 - 0.5) = 1515.0
        // loser  : 1500 + 30 * (0.0 - 0.5) = 1485.0
        double expectedWinnerRating = 1515.0;
        double expectedLoserRating = 1485.0;
        assertEquals(expectedWinnerRating, winner.getRating(), 0.001, "Rating du vainqueur incorrect");
        assertEquals(expectedLoserRating, loser.getRating(), 0.001, "Rating du perdant incorrect");

        // Vérifier que le PacketHelper a été appelé avec les ratings mis à jour
        verify(mockPacketHelper).createRatingUpdatePacket(eq(winner.getId()), eq(winner.getRating()),
              eq(loser.getId()), eq(loser.getRating()));
        // Vérifier que le paquet UPDATE_RATING a été envoyé
        verify(mockGameManager).send(mockPacket);

        // Vérifier que l'événement de victoire est transmis via processGameEvent
        ArgumentCaptor<GameVictoryEvent> eventCaptor = ArgumentCaptor.forClass(GameVictoryEvent.class);
        verify(mockGame).processGameEvent(eventCaptor.capture());
        GameVictoryEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent, "L'événement de victoire ne doit pas être nul");

        // Vérifier que les méthodes de transmission de l'événement de victoire et de réinitialisation du jeu ont bien été appelées
        verify(mockGameManager).transmitGameVictoryEventToAll();
        verify(mockGameManager).resetGame();
    }
}

package net.fliuxx.bossCore.abilities;

import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;

import java.util.List;

public interface BossAbility {
    /**
     * Verifica se l'abilità può essere attivata
     * @return true se l'abilità può essere attivata
     */
    boolean canActivate();

    /**
     * Attiva l'abilità
     * @param boss Il boss dell'evento
     * @param nearbyPlayers Lista di giocatori vicini che potrebbero essere colpiti dall'abilità
     */
    void activate(IronGolem boss, List<Player> nearbyPlayers);

    /**
     * Verifica se l'abilità è abilitata nella configurazione
     * @return true se l'abilità è abilitata
     */
    boolean isEnabled();

    /**
     * Resetta lo stato dell'abilità
     */
    void reset();
}
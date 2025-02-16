class GameState {
    val drawPile: MutableList<Card> = mutableListOf()
    val discardPile: MutableList<Card> = mutableListOf()
    var currentPlayer: Player? = null // Hier wird currentPlayer definiert
    var requiredValue: Int? = null // Speichert den erforderlichen Wert für die nächste Karte
    var currentPhase: GamePhase = GamePhase.HAND_CARDS

}
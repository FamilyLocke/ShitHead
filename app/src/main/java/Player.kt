
    class Player(val name: String, val isBot: Boolean =false) {
        val hand: MutableList<Card> = mutableListOf()
        val openCards: MutableList<Card> = mutableListOf()
        val hiddenCards: MutableList<Card> = mutableListOf()
        val selectedOpenCards: MutableList<Card> = mutableListOf()

}
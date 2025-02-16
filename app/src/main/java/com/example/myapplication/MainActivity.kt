package com.example.myapplication

import Card
import GameLogic
import GameState
import Player
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var gameLogic: GameLogic
    private lateinit var players: List<Player>
    private lateinit var gameState: GameState

    // UI-Elemente als Klassenvariablen
    private lateinit var imagePlayedStack: ImageView
    private lateinit var imageBurnedStack: ImageView
    private lateinit var imageDrawStack: ImageView

    private lateinit var closedCard1: ImageView
    private lateinit var closedCard2: ImageView
    private lateinit var closedCard3: ImageView

    private lateinit var opendCard1: ImageView
    private lateinit var opendCard2: ImageView
    private lateinit var opendCard3: ImageView

    private lateinit var  openCardsLayout: LinearLayout
    private lateinit var  closedCards: LinearLayout
    private lateinit var  hand: LinearLayout



    private val selectedOpenCards = mutableListOf<Card>()
    private lateinit var confirmSelectionButton: Button
    private lateinit var takeCardsButton: Button
    private lateinit var gameinfo: Button

    private fun onHandCardClicked(player: Player, selectedCard: Card) {
        // Zähle, wie oft die Karte mit diesem Wert in der Hand vorkommt
        val matchingCards = player.hand.filter { it.value == selectedCard.value }

        // Falls mehrere Karten mit gleichem Wert vorhanden sind, alle spielen
        if (matchingCards.size > 1) {
            if (matchingCards.all { gameLogic.isValidMove(it) }) {
                matchingCards.forEach { card ->
                    gameLogic.playCard(player, card)
                }
                Log.d("MainActivity", "${player.name} spielt ${matchingCards.size} Karten mit Wert ${selectedCard.value}")
            } else {
                Toast.makeText(this, "Karten können nicht zusammen gespielt werden!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Falls nur eine Karte vorhanden ist, normal spielen
            if (gameLogic.isValidMove(selectedCard)) {
                gameLogic.playCard(player, selectedCard)
            }
        }

        updateUI() // UI aktualisieren
    }

    private fun selectCardForOpenCards(card: Card, button: ImageView) {
        if (selectedOpenCards.contains(card)) {
            selectedOpenCards.remove(card)
            button.alpha = 1.0f // Entferne optische Hervorhebung
        } else {
            if (selectedOpenCards.size < 3) {
                selectedOpenCards.add(card)
                button.alpha = 0.5f // Markiere Karte als ausgewählt
            }
        }
    }

    private fun setDoubleClickListener(view: View, onSingleClick: () -> Unit, onDoubleClick: () -> Unit) {
        var lastClickTime: Long = 0
        val doubleClickThreshold = 300L // Explizit als Long

        view.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime < doubleClickThreshold) {
                onDoubleClick() // Doppelklick erkannt
            } else {
                view.postDelayed({
                    if (System.currentTimeMillis() - lastClickTime >= doubleClickThreshold) {
                        onSingleClick() // Einzelklick nur ausführen, wenn kein zweiter Klick kommt
                    }
                }, doubleClickThreshold) // Explizite Konvertierung zu Long
            }

            lastClickTime = currentTime
        }
    }




    private fun updateUI() {
        Log.d("updateUI", "Updating UI for player: ${players[0].name}")
        val currentPlayer = gameLogic.players[0]

        // Prüfe, ob der Spieler in die nächste Phase darf
        gameLogic.checkNextPhase(currentPlayer!!)

        when (gameState.currentPhase) {
            GamePhase.HAND_CARDS -> updateHandCards(currentPlayer)
            GamePhase.OPEN_CARDS -> updateOpenCards(currentPlayer)
            GamePhase.CLOSED_CARDS -> updateClosedCards(currentPlayer)
        }

        updateHandCards(currentPlayer)
        updateOpenCards(currentPlayer)
        updateClosedCards(currentPlayer)
        updatePlayedStack()
        updateDrawStack()
        updateBurnedStack()

        checkForWinner()

    }
    private fun checkForWinner() {
        for (player in players) {
            // Ein Spieler gewinnt, wenn er keine Handkarten, keine offenen und keine verdeckten Karten mehr hat
            if (player.hand.isEmpty() && player.openCards.isEmpty() && player.hiddenCards.isEmpty()) {
                Toast.makeText(this, "${player.name} hat gewonnen!", Toast.LENGTH_LONG).show()
                Log.d("GAME", "${player.name} hat das Spiel gewonnen!")

                // Optional: Spiel beenden oder Reset einleiten
                return
            }
        }
    }

    // Hilfsfunktion: Handkarten aktualisieren
    private fun updateHandCards(player: Player) {
        hand.removeAllViews()

        for (card in player.hand) {
            val imageView = ImageView(this)
            val resourceName = "the${card.suit}of${card.value}"
            val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)

            imageView.setImageResource(resourceId)

            // Größe der Karte setzen
            val layoutParams = LinearLayout.LayoutParams(160, 240) // Breite & Höhe anpassen
            layoutParams.setMargins(8, 0, 8, 0) // Abstand zwischen Karten
            imageView.layoutParams = layoutParams


            setDoubleClickListener(imageView,
                onSingleClick = { gameLogic.playCard(player, card); updateUI() },
                onDoubleClick = { onHandCardClicked(player, card);updateUI()}

            )
            hand.addView(imageView)

        }

    }

    // Hilfsfunktion: Offene Karten aktualisieren
    private fun updateOpenCards(player: Player) {
        // Zugriff auf die offenen Karten des Spielers
        val openCards = player.openCards
        // Liste der ImageViews für offene Karten
        val cardImageViews = listOf(opendCard1, opendCard2, opendCard3)

        // Durchlaufe die offenen Karten und setze sie auf die entsprechenden ImageViews
        for (i in 0..2) {
            if (i < openCards.size) {
                // Hole die Karte für das aktuelle ImageView
                val card = openCards[i]
                val resourceName = "the${card.suit}of${card.value}"
                val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)

                // Debugging: Überprüfe den Ressourcen-Namen und die ID
                Log.d("updateOpenCards", "Resource name: $resourceName, Resource ID: $resourceId")

                if (resourceId != 0) {
                    // Setze das Kartenbild auf die entsprechende ImageView
                    cardImageViews[i].setImageResource(resourceId)
                    // Stelle sicher, dass die Sichtbarkeit der ImageView auf VISIBLE gesetzt wird
                    cardImageViews[i].visibility = View.VISIBLE
                    Log.d("updateOpenCards", "Set image resource for card $i")
                } else {
                    Log.d("updateOpenCards", "Resource not found for $resourceName")
                    // Falls keine Ressource gefunden wird, unsichtbar machen
                    cardImageViews[i].visibility = View.INVISIBLE
                }
            } else {
                // Wenn weniger als 3 Karten vorhanden sind, setze die restlichen ImageViews auf unsichtbar
                cardImageViews[i].visibility = View.INVISIBLE
            }
        }
    }

    // Hilfsfunktion: Verdeckte Karten aktualisieren
    private fun updateClosedCards(player: Player) {
        // Zugriff auf die offenen Karten des Spielers
        val hiddenCards = player.hiddenCards
        // Liste der ImageViews für offene Karten
        val cardImageViews = listOf(closedCard1, closedCard2, closedCard3)
        // Durchlaufe die offenen Karten und setze sie auf die entsprechenden ImageViews
        for (i in 0..2) {
            if (i < hiddenCards.size) {
                // Hole die Karte für das aktuelle ImageView
                val card = hiddenCards[i]
                val resourceName = "the${card.suit}of${card.value}"
                val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)

                // Debugging: Überprüfe den Ressourcen-Namen und die ID
                Log.d("updateOpenCards", "Resource name: $resourceName, Resource ID: $resourceId")

                if (resourceId != 0) {
                    // Setze das Kartenbild auf die entsprechende ImageView
                    cardImageViews[i].setImageResource(resourceId)
                    // Stelle sicher, dass die Sichtbarkeit der ImageView auf VISIBLE gesetzt wird
                    cardImageViews[i].visibility = View.VISIBLE
                    Log.d("updateOpenCards", "Set image resource for card $i")
                } else {
                    Log.d("updateOpenCards", "Resource not found for $resourceName")
                    // Falls keine Ressource gefunden wird, unsichtbar machen
                    cardImageViews[i].visibility = View.INVISIBLE
                }
            } else {
                // Wenn weniger als 3 Karten vorhanden sind, setze die restlichen ImageViews auf unsichtbar
                cardImageViews[i].visibility = View.INVISIBLE
            }
        }

    }



    // Hilfsfunktion: Ablagestapel (Played Stack) aktualisieren
    private fun updatePlayedStack() {
        val discardPile = gameState.discardPile
        val topCard = discardPile.lastOrNull() // Oberste Karte
        val secondLastCard = if (discardPile.size > 1) discardPile[discardPile.size - 2] else null // Vorletzte Karte

        if (topCard != null) {
            // Standardmäßig die oberste Karte anzeigen
            val topResourceName = "the${topCard.suit}of${topCard.value}"
            val topResourceId = resources.getIdentifier(topResourceName, "drawable", packageName)

            if (topResourceId != 0) {
                imagePlayedStack.setImageResource(topResourceId)
                imagePlayedStack.alpha = 1.0f // Standardmäßig volle Sichtbarkeit
            } else {
                imagePlayedStack.setImageResource(R.drawable.defaultstack)
            }

            // Falls die oberste Karte eine 3 ist, zeige stattdessen die vorletzte Karte leicht durchsichtig an
            if (topCard.value == 3 && secondLastCard != null) {
                val secondLastResourceName = "the${secondLastCard.suit}of${secondLastCard.value}"
                val secondLastResourceId = resources.getIdentifier(secondLastResourceName, "drawable", packageName)

                if (secondLastResourceId != 0) {
                    imagePlayedStack.setImageResource(secondLastResourceId)
                    imagePlayedStack.alpha = 0.5f // Vorletzte Karte leicht durchsichtig
                } else {
                    imagePlayedStack.setImageResource(R.drawable.defaultstack)
                    imagePlayedStack.alpha = 0.5f
                }
            }
        } else {
            // Falls der Ablagestapel leer ist, zeige eine leere Karte
            imagePlayedStack.setImageResource(R.drawable.defaultstack)
            imagePlayedStack.alpha = 1.0f
        }
    }






    // Hilfsfunktion: Ziehstapel (Draw Stack) aktualisieren
    private fun updateDrawStack() {
        if (gameState.drawPile.isEmpty()) {
            imageDrawStack.visibility = View.INVISIBLE
        }

    }

    // Hilfsfunktion: Verbrannte Karten (Burned Stack) aktualisieren
    private fun updateBurnedStack() {
        // Beispiel: Verbrannte Karten anzeigen (falls implementiert)
        imageBurnedStack.setImageResource(R.drawable.burnedstack)
    }

    private fun confirmOpenCardSelection() {
        val currentPlayer = gameState.currentPlayer // Nur für den ersten Spieler

        if (selectedOpenCards.size == 3) {
            // Füge die Karten zu den offenen Karten des Spielers hinzu
            currentPlayer?.openCards?.addAll(selectedOpenCards)
            // Entferne die ausgewählten Karten aus der Hand des Spielers
            currentPlayer?.hand?.removeAll(selectedOpenCards)
            // Leere die Auswahl
            selectedOpenCards.clear()
            // UI aktualisieren
            updateUI()
            // Bestätigungsbutton deaktivieren, falls nicht mehr benötigt
            confirmSelectionButton.visibility = View.INVISIBLE

        } else {
            Toast.makeText(this, "Bitte genau 3 Karten auswählen!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restartGame() {
        val intent = Intent(this, MainActivity::class.java)
        finish() // Beende die aktuelle Aktivität
        startActivity(intent) // Starte eine neue Instanz der MainActivity
    }



    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitiy_game)

        // UI-Elemente mit findViewById verbinden
        imagePlayedStack = findViewById(R.id.discardPile)
        imageDrawStack = findViewById(R.id.imageDrawStack)
        imageBurnedStack = findViewById(R.id.imageBurnedStack)
        openCardsLayout = findViewById(R.id.openCardsLayout)
        closedCards = findViewById(R.id.hiddenCardsLayout) // Füge diese Zeile hinzu!


        hand = findViewById(R.id.handLayout) as LinearLayout

        closedCard1 = findViewById(R.id.closedCard1)
        closedCard2 = findViewById(R.id.closedCard2)
        closedCard3 = findViewById(R.id.closedCard3)


        opendCard1 = findViewById(R.id.opendCard1)
        opendCard2 = findViewById(R.id.opendCard2)
        opendCard3 = findViewById(R.id.opendCard3)

        confirmSelectionButton = findViewById(R.id.confirmSelectionButton)
        confirmSelectionButton.setOnClickListener {
            confirmOpenCardSelection()
        }

        takeCardsButton = findViewById(R.id.takeCardsButton)
            takeCardsButton.setOnClickListener {
                val currentPlayer = gameLogic.players[0] ?: return@setOnClickListener

                // Prüfen, ob der Spieler eine gültige Karte spielen kann
                if (!gameLogic.canPlay(currentPlayer)) {
                    // Falls er nicht legen kann, nimmt er alle Karten vom Ablagestapel
                    currentPlayer.hand.addAll(gameState.discardPile)
                    gameState.discardPile.clear() // Ablagestapel leeren

                    Toast.makeText(this, "${currentPlayer.name} konnte nicht legen und nimmt alle Karten!", Toast.LENGTH_SHORT).show()
                    Log.d("GAME", "${currentPlayer.name} nimmt alle Karten auf!")

                    // UI aktualisieren
                    updateUI()
                } else {
                    Toast.makeText(this, "Du kannst eine Karte spielen!", Toast.LENGTH_SHORT).show()
                    Log.d("GAME", "${currentPlayer.name} kann noch legen, keine Karten aufgenommen.")
                }

                gameState.currentPhase = GamePhase.HAND_CARDS
                gameLogic.nextPlayer()
                updateUI()
            }



        gameinfo = findViewById(R.id.gameInfo)
        gameinfo.setOnClickListener {
            Log.d("SPIELSTAND", gameLogic.getPlayersState())
            updateUI()
        }



        // Spiel initialisieren

        val bot = Player("Bot", isBot = true)
        players = listOf(Player("Spieler"), bot)
        gameState = GameState()
        gameLogic = GameLogic(players, gameState, this)
        gameLogic.updateUICallback = { runOnUiThread { updateUI() } } // Callback setzen
        gameLogic.startGame()


        imagePlayedStack.setOnClickListener{
            restartGame()

        }
        // Klick-Listener für Karten
        imageBurnedStack.setOnClickListener {
            gameState.drawPile.clear()
            gameLogic.players[0].hand.clear()
            gameLogic.players[1].hand.clear()
            updateUI()

        }

        // Stelle sicher, dass der Spieler genau 6 Karten in der Hand hat
        for (i in 0 until (gameLogic.players[0].hand.size!!)) {
            val card = gameLogic.players[0].hand?.get(i)
            val imageView = ImageView(this)

            // Bestimme den Namen der Ressource basierend auf dem Wert und dem Symbol der Karte
            val resourceName = "the${card?.suit}of${card?.value}"
            val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)

            // Setze das Bild der Karte
            imageView.setImageResource(resourceId)

            // Setze Layout-Parameter für das Bild
            val layoutParams = LinearLayout.LayoutParams(160, 240) // Größe der Karte
            layoutParams.setMargins(8, 0, 8, 0)  // Abstand zwischen den Karten
            imageView.layoutParams = layoutParams

            // Füge die Karte zur Handansicht hinzu
            hand.addView(imageView)

            // Füge den Listener für jede Handkarte hinzu
            imageView.setOnClickListener {
                if (card != null) {
                    selectCardForOpenCards(card, imageView)
                } // Dies führt die Auswahl für diese Karte aus
            }
        }

        opendCard1.setOnClickListener {
            if (gameState.currentPhase == GamePhase.OPEN_CARDS) {
                val card = gameState.currentPlayer!!.openCards.getOrNull(0) ?: return@setOnClickListener
                gameLogic.playOpenCard(gameState.currentPlayer!!, card)
                updateUI()
            }
        }

        opendCard2.setOnClickListener {
            if (gameState.currentPhase == GamePhase.OPEN_CARDS) {
                val card = gameState.currentPlayer!!.openCards.getOrNull(1) ?: return@setOnClickListener
                gameLogic.playOpenCard(gameState.currentPlayer!!, card)
                updateUI()
            }
        }

        opendCard3.setOnClickListener {
            if (gameState.currentPhase == GamePhase.OPEN_CARDS) {
                val card = gameState.currentPlayer!!.openCards.getOrNull(2) ?: return@setOnClickListener
                gameLogic.playOpenCard(gameState.currentPlayer!!, card)
                updateUI()
            }
        }

        closedCard1.setOnClickListener {
            if (gameState.currentPhase == GamePhase.CLOSED_CARDS) {
                val card = gameState.currentPlayer!!.hiddenCards.getOrNull(0) ?: return@setOnClickListener
                gameLogic.playClosedCard(gameState.currentPlayer!!, card)
                updateUI()
            }
        }
        closedCard2.setOnClickListener {
            if (gameState.currentPhase == GamePhase.CLOSED_CARDS) {
                val card = gameState.currentPlayer!!.hiddenCards.getOrNull(1) ?: return@setOnClickListener
                gameLogic.playClosedCard(gameState.currentPlayer!!, card)
                updateUI()
            }
        }
        closedCard3.setOnClickListener {
            if (gameState.currentPhase == GamePhase.CLOSED_CARDS) {
                val card = gameState.currentPlayer!!.hiddenCards.getOrNull(2) ?: return@setOnClickListener
                gameLogic.playClosedCard(gameState.currentPlayer!!, card)
                updateUI()
            }
        }

        imageDrawStack.setOnClickListener{
            gameLogic.players[0]?.let { it1 -> gameLogic.drawCardFromStack(it1)
            updateUI()
            }
        }
    }
}

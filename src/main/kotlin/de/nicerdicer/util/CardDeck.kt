package de.nicerdicer.util

import dev.kord.x.emoji.Emojis

class CardDeck {
    val cards = mutableListOf<Card>()

    init {
        for (suit in Suit.entries) {
            if (suit == Suit.RED || suit == Suit.BLACK) continue
            for (rank in Rank.entries) {
                if (rank == Rank.JOKER) continue
                cards.add(Card(suit, rank))
            }
        }
        cards.add(Card(Suit.BLACK, Rank.JOKER))
        cards.add(Card(Suit.RED, Rank.JOKER))
    }

    fun shuffle() = cards.shuffle()

    fun draw(): Card? = cards.removeFirstOrNull()

    fun insertCardAt(index: Int, card: Card) = cards.add(index, card)
}

class CardHand {
    val handSize = 5
    val cards = mutableListOf<Card>()

    fun addCard(card: Card) = cards.add(card)

    fun removeCardAt(index: Int) = if (0 <= index && index <= cards.lastIndex) cards.removeAt(index) else null

    fun sort()
    {
        cards.sortBy { it.rank }
        cards.sortBy { it.suit }
    }

    fun getHand() = cards
}

class Card(val suit: Suit, val rank: Rank) {
    fun toLongString() = if (rank == Rank.JOKER) "${suit.asEmoji()} ${rank.asName()}" else "${rank.asName()} of ${suit.asEmoji()}"
    override fun toString(): String = "${suit.asEmoji()}${rank.asShortName()}"
}

enum class Suit {
    CLUBS {
        override fun asEmoji() = Emojis.clubs.unicode
    },
    DIAMONDS {
        override fun asEmoji() = Emojis.diamonds.unicode
    },
    HEARTS {
        override fun asEmoji() = Emojis.hearts.unicode
    },
    SPADES {
        override fun asEmoji() = Emojis.spades.unicode
    },
    RED {
        override fun asEmoji() = Emojis.redCircle.unicode
    },
    BLACK {
        override fun asEmoji() = Emojis.blackCircle.unicode
    };

    abstract fun asEmoji(): String
}

enum class Rank(number: Int) {
    JOKER(0) {
        override fun asName() = "Joker"
        override fun asShortName() = Emojis.blackJoker.unicode
    },
    ACE(1) {
        override fun asName() = "Ace"
        override fun asShortName() = "A"
    },
    TWO(2) {
        override fun asName() = "2"
        override fun asShortName() = "2"
    },
    THREE(3) {
        override fun asName() = "3"
        override fun asShortName() = "3"
    },
    FOUR(4) {
        override fun asName() = "4"
        override fun asShortName() = "4"
    },
    FIVE(5) {
        override fun asName() = "5"
        override fun asShortName() = "5"
    },
    SIX(6) {
        override fun asName() = "6"
        override fun asShortName() = "6"
    },
    SEVEN(7) {
        override fun asName() = "7"
        override fun asShortName() = "7"
    },
    EIGHT(8) {
        override fun asName() = "8"
        override fun asShortName() = "8"
    },
    NINE(9) {
        override fun asName() = "9"
        override fun asShortName() = "9"
    },
    TEN(10) {
        override fun asName() = "10"
        override fun asShortName() = "10"
    },
    JACK(11) {
        override fun asName() = "Jack"
        override fun asShortName() = "J"
    },
    QUEEN(12) {
        override fun asName() = "Queen"
        override fun asShortName() = "Q"
    },
    KING(13) {
        override fun asName() = "King"
        override fun asShortName() = "K"
    };

    abstract fun asName(): String

    abstract fun asShortName(): String
}
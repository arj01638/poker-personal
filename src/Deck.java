import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class Deck {

	final LinkedList<Card> mainDeck = new LinkedList<>();

	Deck(Card[] blacklist) {
		this.clear(blacklist);
	}

	Deck(boolean clear) {
		if (clear) this.clear();
	}
	
	public Card randomCard() {
		Card c;
		try {
			c = mainDeck.remove(ThreadLocalRandom.current().nextInt(0, mainDeck.size()));
		} catch(IllegalArgumentException e) {
			//e.printStackTrace();
			return null;
		}
		return c;
	}
	
	public void clear() {
		mainDeck.clear();
		for (int i = 2; i < 15; i++) {
			for (int k = 1; k < 5; k++) {
				mainDeck.add(new Card(i,k));
			}
		}
	} // clear

	public void clear(Card[] blacklist) {
		mainDeck.clear();
		for (int i = 2; i < 15; i++) {
			for (int k = 1; k < 5; k++) {
				boolean add = true;
				for (Card card : blacklist) {
					if (card.val == i && card.suit == k) {
						add = false;
						break;
					}
				}
				if (add) mainDeck.add(new Card(i, k));
			}
		}
	} // clear

	public void add(Card c) {
		mainDeck.add(c);
	}

	public Deck deepCopy() {
		Deck deck = new Deck(false);
		for (Card card : mainDeck) {
			deck.add(card.copyOf());
		}
		return deck;
	}

}


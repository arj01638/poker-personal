import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class Deck {

	final LinkedList<Card> mainDeck = new LinkedList<>();
	Deck() {
		this.clear();
	}

	Deck(Card[] blacklist) {
		this.clear(blacklist);
	}

	Deck(boolean clear) {
		if (clear) this.clear();
	}
	
	public Card randomCard() {
		Card c = null;
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

	public int size() {
		return mainDeck.size();
	}
	public Card get(int i) {
		return mainDeck.get(i);
	}
	public Deck remove(Card c) {
		for (int i = 0; i < mainDeck.size(); i++) {
			Card d = mainDeck.get(i);
			if (d.val == c.val && d.suit == c.suit) {
				mainDeck.remove(i);
				return this;
			}
		}
		throw new RuntimeException("tried to remove card that doesnt exist");
	}

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


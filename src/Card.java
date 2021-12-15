
public class Card implements Comparable<Card> {

	final int val;
	final int suit;
	
	Card(int value, int suit) {
		this.val = value;
		this.suit = suit;
	} // Card
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		switch (val) {
			case 11 -> string.append("J");
			case 12 -> string.append("Q");
			case 13 -> string.append("K");
			case 14 -> string.append("A");
			default -> string.append(val);
		}
		switch (suit) {
			case 1 -> string.append("s");
			case 2 -> string.append("c");
			case 3 -> string.append("h");
			case 4 -> string.append("d");
		}
		return string.toString();
	}

	@Override
	public int compareTo(Card o) {
		return o.val - this.val;
	}

	public Card copyOf() {
		return new Card(this.val, this.suit);
	}
	
}

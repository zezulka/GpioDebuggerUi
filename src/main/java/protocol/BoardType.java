package protocol;

/**
 *
 * @author Miloslav Zezulka, 2017
 */
public enum BoardType {
    RASPBERRY_PI("Raspberry Pi"),
    BEAGLEBONEBLACK("BeagleBone Black"),
    CUBIEBOARD("Cubieboard");

    private final String name;

    private BoardType(String name) {
        this.name = name;
    }

    private String getName() {
        return this.name;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    /**
     * Parses string representing name of the Board (as declared in bulldog library).
     * This method is usually used for parsing the first received message from agent.
     * @param name
     * @return 
     */
    public static BoardType parse(String name) {
        if(name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        for(BoardType t : BoardType.values()) {
            if(t.getName().equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException(name + " is not a BoardType!");
    }
}

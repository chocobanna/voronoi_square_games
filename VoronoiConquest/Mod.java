public interface Mod {
    // Called when the mod is loaded.
    void init(GameEngine engine);
    
    // Returns the name of the mod.
    String getName();
}

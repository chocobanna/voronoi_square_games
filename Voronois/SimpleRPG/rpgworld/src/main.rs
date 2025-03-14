mod character;
mod character_creation;
mod dungeon;

fn main() {
    let hero = character_creation::create_character();
    println!("Your character: {:?}", hero);

    dungeon::enter(&hero);
}

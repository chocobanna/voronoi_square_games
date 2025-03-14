pub mod encounter;

pub fn enter(character: &crate::character::Character) {
    println!("{} enters the dungeon.", character.name);
    encounter::start_encounter(character);
}

use crate::character::{Character, Role, Race};
use std::io::{self, Write};

#[allow(dead_code)]
pub fn create_character() -> Character{
    let name = read_input("Enter your character's name:");
    let role = select_role();
    let race = select_race();
    Character{
        name,
        level: 1,
        race,
        role,
        strength: 1,
        dexterity: 1,
        constituition: 1,
        intelligence: 1,
    }
}

fn read_input(prompt: &str) -> String{
    let mut input = String::new();
    print!("{}",prompt);
    io::stdout().flush().expect("Failed to flush stdout.");
    io::stdin().read_line(&mut input).expect("Failed to read line.");
    input.trim().to_string()
}

fn select_role() -> Role{
    println!("Choose your class!:");
    println!("1 - Warrior");
    println!("2 - Mage");
    println!("3 - Rogue");
    println!("4 - Acolyte");
    let class = read_input("Enter the number corresponding to your class:");
    match class.trim().parse::<u32>(){
        Ok(1) => Role::Warrior,
        Ok(2) => Role::Mage,
        Ok(3) => Role::Rogue,
        Ok(4) => Role::Acolyte,
        _ => {
            println!("No class chosen, defaulting to Warrior.");
            Role::Warrior
        }
    }

}

fn select_race() -> Race{
    println!("Choose your class!:");
    println!("1 - Human");
    println!("2 - Elf");
    println!("3 - Dwarf");
    let race = read_input("Enter the number corresponding to your race:");
    match race.trim().parse::<u32>(){
        Ok(1) => Race::Human,
        Ok(2) => Race::Elf,
        Ok(3) => Race::Dwarf,
        _ => {
            println!("No race chosen, defaulting to Human.");
            Race::Human
        }
    }

}

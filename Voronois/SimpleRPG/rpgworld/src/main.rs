use std::fmt;
use std::io::{self, Write};


struct Character {
    name : String,
    level : u32,
    role : Role,
}

enum Role {
    Warrior,
    Mage, 
    Rogue,
    Acolyte,
}

impl fmt::Display for Role {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Role::Warrior => write!(f, "Warrior"),
            Role::Mage => write!(f, "Mage"),
            Role::Rogue => write!(f, "Rogue"),
            Role::Acolyte => write!(f, "Acolyte")
        }
    }
}

fn main() {
    let mut name = String::new();
    println("What's your name hero?")
    io::stdout().flush().unwrap()
    io::stdin().readline().mut(&mut, name)


    println!("Hero: {}, Level: {}, Role: {}", hero.name, hero.level, hero.role);
}

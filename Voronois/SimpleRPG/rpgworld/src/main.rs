use std::fmt;
use std::io::{self, Write};

#[derive(Debug)]
#[allow(dead_code)]
struct Character {
    name : String,
    level : u32,
    role : Role,
}

#[derive(Debug)]
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



fn main() {
    let name = read_input("Enter your character's name:");
    let role = select_role();
    let hero = Character{
        name,
        level: 1,
        role,
    };

    println!("Your character : {:?}", hero);
}

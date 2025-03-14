use std::fmt;


#[derive(Debug)]
#[allow(dead_code)]
pub struct Character {
    pub name : String,
    pub level : u32,
    pub race : Race,
    pub role : Role,
    pub strength : u32,
    pub dexterity : u32,
    pub constitution : u32,
    pub intelligence : u32,
}

impl fmt::Display for Character {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "Your character:")?;
        writeln!(f, "  Name         : {}", self.name)?;
        writeln!(f, "  Level        : {}", self.level)?;
        writeln!(f, "  Race         : {}", self.race)?;
        writeln!(f, "  Role         : {}", self.role)?;
        writeln!(f, "  Strength     : {}", self.strength)?;
        writeln!(f, "  Dexterity    : {}", self.dexterity)?;
        writeln!(f, "  Constitution : {}", self.constitution)?;
        writeln!(f, "  Intelligence : {}", self.intelligence)
    }
}

#[derive(Debug)]
pub enum Role {
    Warrior,
    Mage, 
    Rogue,
    Acolyte,
}

impl std::fmt::Display for Role {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Role::Warrior => write!(f, "Warrior"),
            Role::Mage    => write!(f, "Mage"),
            Role::Rogue   => write!(f, "Rogue"),
            Role::Acolyte => write!(f, "Acolyte")
        }
    }
}

#[derive(Debug)]
pub enum Race {
    Human,
    Elf, 
    Dwarf,
}

impl std::fmt::Display for Race {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Race::Human => write!(f, "Human"),
            Race::Elf   => write!(f, "Elf"),
            Race::Dwarf => write!(f, "Dwarf"),
        }
    }
}
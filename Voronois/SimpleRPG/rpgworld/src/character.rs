#[derive(Debug)]
#[allow(dead_code)]
pub struct Character {
    pub name : String,
    pub level : u32,
    pub race : Race,
    pub role : Role,
    pub strength : u32,
    pub dexterity : u32,
    pub constituition : u32,
    pub intelligence : u32,
}

#[derive(Debug)]
pub enum Role {
    Warrior,
    Mage, 
    Rogue,
    Acolyte,
}

#[derive(Debug)]
pub enum Race {
    Human,
    Elf, 
    Dwarf,
}
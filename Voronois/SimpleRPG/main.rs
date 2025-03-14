fn print_level(level: &i32) {
    println!("Current level: {}", level);
}

fn main() {
    let mut level = 1;
    print_level(&level);
    level += 1;
    print_level(&level);
    level += 1;
    print_level(&level);
    level += 1;
    print_level(&level);
    level += 1;
}

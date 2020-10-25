// run-rustfix
#![allow(dead_code)]

struct Foo {
    v: Vec<u32>,
}

impl Foo {
    fn bar(&self) {
        for _ in self.v { //~ ERROR cannot move out of `self.v` which is behind a shared reference
        }
    }
}

fn main() {}

/* Test while loops. */

function helloWorld(animal) {
    "use strict";

    function speak(animal, noise) {
        console.log("The " + animal + " says " + noise + ".");
    }

    var i = 0, noise = "moo";
    while(i < 10) {
        speak(animal, noise);
        if(i === 5) {
            i++;
            continue;
        }
        noise = noise + "o";
        i = i + 1;
    }

}

var animal = "Cow";
helloWorld(animal);

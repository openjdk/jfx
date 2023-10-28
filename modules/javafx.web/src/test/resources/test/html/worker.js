/* simple worker module to test*/

var i = 0;
let timeoutID;

function doIncrement() {
  i = i + 1;
  postMessage(i);

  if (i < 4) {
    // If i is less than 4, schedule the next call
    timeoutID = setTimeout(doIncrement, 50);
  }
}

doIncrement();


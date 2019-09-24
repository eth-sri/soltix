contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    emit E(fib(x));
    return x;
  }

  function fib(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    if (x == uint(0)) return 0;
    else if (x == uint(1)) return 1;
    else return fib(x - uint(1)) + fib(x - uint(2));
  }
}

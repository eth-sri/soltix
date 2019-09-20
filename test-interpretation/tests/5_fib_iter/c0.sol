contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    emit E(fib(x));
    return x;
  }

  function fib(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    uint prevPrev = uint(0);
    uint prev = uint(1);
    uint cur = uint(0);
    uint i = uint(1);

    while (i < x) {
      cur = prev + prevPrev;
      prevPrev = prev;
      prev = cur;
      ++i;
    }

    return cur;
  }
}

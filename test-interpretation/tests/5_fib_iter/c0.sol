contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    emit E(fib(x));
    emit E(fib2(x+uint(1)));
    emit E(fib3(x+uint(3)));
    emit E(fib4(x+uint(3)));
    emit E(fib5(x+uint(4)));
    emit E(fib6(x+uint(5)));
    return x;
  }

  ////////////////////////// while /////////////////////////////
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
  function fib2(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    uint prevPrev = uint(0);
    uint prev = uint(1);
    uint cur = uint(0);
    uint i = uint(1);

    while (i++ < x) { // variant: side effect in condition, not at end of loop
      cur = prev + prevPrev;
      prevPrev = prev;
      prev = cur;
    }

    return cur;
  }
  function fib3(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    uint prevPrev = uint(0);
    uint prev = uint(1);
    uint cur = uint(0);
    uint i = uint(1);

    while (true) { // variant: side effect in condition, not at end of loop
      cur = prev + prevPrev;
      prevPrev = prev;
      prev = cur;
      if (i++ == x) {
         break;
      }
    }

    return cur;
  }


  ////////////////////////// for /////////////////////////////
  function fib4(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    uint prevPrev = uint(0);
    uint prev = uint(1);
    uint cur = uint(0);

    for (uint i = uint(1); i < x; ++i) {
      cur = prev + prevPrev;
      prevPrev = prev;
      prev = cur;
    }

    return cur;
  }
  function fib5(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    uint prevPrev = uint(0);
    uint prev = uint(1);
    uint cur = uint(0);

    for (uint i = uint(1); true; ++i) {
      cur = prev + prevPrev;
      prevPrev = prev;
      prev = cur;
      if (i == x) break;
    }

    return cur;
  }

  ////////////////////////// do-while /////////////////////////////
  function fib6(uint x) private returns (uint) {
    // Note: type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    uint prevPrev = uint(0);
    uint prev = uint(1);
    uint cur = uint(0);
    uint i = uint(1);
    do {
      cur = prev + prevPrev;
      prevPrev = prev;
      prev = cur;
    } while (++i < x);

    return cur;
  }
}

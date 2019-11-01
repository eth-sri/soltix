
contract c0 {
  int a;
  // TODO "this" semantics - this.a is not accepted by solc, but we do?
  constructor(int aval) public { a = aval; }

  event E(int a);

  function f(int addend) public {
     emit E(a += addend);
  }
}


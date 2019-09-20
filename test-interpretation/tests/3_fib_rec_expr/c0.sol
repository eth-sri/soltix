contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    emit E(fib(x));
    return x;
  }

  function fib(uint x) private returns (uint) {
     /*
     if (x == 0) return 0;
     else if (x == 1) return 1;
     else return fib(x - 1) + fib(x - 2);
     */
    return (x == uint(0)? uint(0): (x == uint(1)? uint(1): fib(x - uint(1)) + fib(x - uint(2)))); // Note: the mere presence of the outer parens introduces an ASTTupleExpression
  }
}

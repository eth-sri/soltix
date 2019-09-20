contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    emit E(fib(x));
    return x;
  }

  function fib(uint x) private returns (uint) {
    // Note:
    //   - the mere presence of the outer parens introduces an ASTTupleExpression
    //   - type inference isn't done yet, so we explicitly ensure that all types are unsigned to avoid type errors
    return (x == uint(0)? uint(0): (x == uint(1)? uint(1): fib(x - uint(1)) + fib(x - uint(2))));
  }
}

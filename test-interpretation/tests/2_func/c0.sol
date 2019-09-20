contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    emit E(c());
    return c();
  }

  function c() public returns (uint) {
    return 123;
  }
}

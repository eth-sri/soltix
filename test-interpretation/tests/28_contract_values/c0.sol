contract c0 {
  event E(uint u);
  function f0(uint x) public payable returns (uint) {
    c1 c = new c1(uint(x + uint(12)));
    emit E(c.getx());
    return c.getx();
  }
}
contract c1 {
   uint public x;
   function getx() public returns (uint) { return x; }
   constructor(uint a) public { x = a; }
}

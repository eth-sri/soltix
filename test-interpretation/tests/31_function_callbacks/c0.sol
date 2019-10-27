
contract c1 {
   uint foo = 3;

   function getFoo() external returns (uint) {
     return foo;
   }
   function getFoo2() external returns (uint) {
     return foo*uint(2);
   }

   function getterCallback() public returns (  function() external returns (uint)   ) {
     return this.getFoo;
   }
   function getterCallback2() public returns (  function() external returns (uint)   ) {
     return this.getFoo2;
   }
}


contract c0 {
   function(uint) external returns (uint) foo;

   event E(uint a);

   function f0(uint x) public {
     c1 c = new c1();
     function() external returns (uint) getter = c.getterCallback();
     emit E(getter() * x);
     emit E(c.getterCallback2()());
   }
}



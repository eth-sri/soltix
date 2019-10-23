
contract c1 {
   uint foo = 3;

   function getFoo() external returns (uint) {
     return foo;
   }

   function getterCallback() public returns (  function() external returns (uint)   ) {
     return this.getFoo;
   }
}


contract c0 {
   function(uint) external returns (uint) foo;

   event E(uint a);

   function f0(uint x) public {
     c1 c = new c1();
     function() external returns (uint) getter = c.getterCallback();
     emit E(getter() * x);
   }

   function f1(uint a) public returns (uint) {
     return a * 2;
   }
   
   function f2(uint a) public returns (uint) {
     return a / 2;
   }
}



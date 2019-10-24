contract c0 {

   function(uint) returns (uint) foo;

   event E(uint a);

   function f0(uint x) public {
     foo = f1;
     emit E(foo(x));
     foo = f2;
     emit E(foo(x));
   }

   function f1(uint a) public returns (uint) {
     return a * uint(2);
   }
   
   function f2(uint a) public returns (uint) {
     return a / uint(2);
   }

}

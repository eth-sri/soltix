contract c0 {
    struct s0 {
        uint248 m0;
        int240 m1;
        bool m2;
    }

    s0 v0 = s0(uint248(101), int240(-74), true);
    bool b = true;

    event E(bool a);
    event E2(uint248 a);
    event E3(int240 a);

    function f0(int16 a0, int136 a1, bytes11 a2, bytes3 a3) public payable {
       emit E( v0.m2 );
       emit E(b);
       emit E2(v0.m0);
       emit E3(v0.m1);
    }
}

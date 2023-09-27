add10:10+;
dup:[->+>+<<]>>[-<<+>>]<<;
print:.;
exit:>60+@1;

// "hi!" - 104 105 33
add10; add10; add10; add10; add10; add10; add10; add10; add10; add10; 4+ dup; print; > + print; > add10; add10; add10; 3+ print;

// "\n" - 10
> add10; print;

> 69+ exit;

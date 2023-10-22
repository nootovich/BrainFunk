add10:10+;
add100:10add10;
dup:[->+>+<<]>>[-<<+>>]<<;
print:.;
exit:>60+@1;

// "hi!" - 104 105 33
add100 4+ dup print > + print > 3add10 3+ print

// "\n" - 10
> add10 print

> 69+ exit

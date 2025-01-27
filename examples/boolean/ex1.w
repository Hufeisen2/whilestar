vars:
  boolean x;
  boolean* y;
  int z;
  boolean[4] a;
code:
  x = false;
  y = &x;
  a[0] = true;
  if((*y == false) and (*a)) {
    print "test true";
  } else {
    print "test false";
  }
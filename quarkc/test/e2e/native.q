int pow(int a, int b);

int pow(int a, int b) for java {
    return (int) Math.pow(a, b);
}
int pow(int a, int b) for javascript {
    return Math.pow(a, b);
}
int pow(int a, int b) for python {
    return a**b
}
int pow(int a, int b) for ruby {
    return a**b
}
int pow(int a, int b) for go
import "math"
{
    return int(math.Pow(float64(a), float64(b)))
}

void testprintany() {
    print(unsafe("yay, it works!!!"));
    assertEqual(1, 1);
}

void testpow() {
    assertEqual(8, pow(2, 3));
    print(unsafe("the result is: " + unsafe(pow(2, 3)).asString()));
}

primitive imaginary
for java {long}
for go {int}
for python {int}
for ruby {Integer}
for javascript {}
{

    imaginary(int n);

    bool __eq__(imaginary o);

    bool __ne__(imaginary o);

    imaginary __add__(imaginary o);

    int foo() {
        return 3;
    }
}

imaginary imaginary___init__(int n);

imaginary imaginary___init__(int n) for go {
    return n;
}
imaginary imaginary___init__(int n) for ruby {
    return n
}
imaginary imaginary___init__(int n) for python {
    return n
}
imaginary imaginary___init__(int n) for java {
    return n;
}
imaginary imaginary___init__(int n) for javascript {
    return n
}

void testimaginary() {
    imaginary x = new imaginary(3);
    assertEqual(1 + 2, x);
}

void test_quark_method_of_primitive() {
    imaginary x = new imaginary(3);
    assertEqual(3, x.foo());
}


void testlist() {
    List<int> li = [];
    List<String> ls = [];
    assertEqual(0, li.size());
    assertEqual(0, ls.size());
    li.append(3);
    ls.append("pi");
    assertEqual(1, li.size());
    assertEqual(1, ls.size());
    assertEqual(3, li[0]);
    assertEqual("pi", ls[0]);
    print(unsafe("--"));
    print(unsafe(ls[0]));
    print(unsafe("--"));
    print(unsafe(li[0]));
}

class Foo {}

void testlistfoo() {
    List<Foo> foos = [new Foo()];
    assertEqual(1, foos.size());
}


void testint_null() {
    int nuffin; // quark vars should initialize to nil value
    assertEqual(nuffin, 0);
}

int theAnswer() {
    return 42;
}

int getTheAnswer();

int getTheAnswer() for java {
    return $theAnswer();
}
int getTheAnswer() for go {
    return $theAnswer()
}
int getTheAnswer() for python {
    return $theAnswer()
}
int getTheAnswer() for ruby {
    return $theAnswer()
}
int getTheAnswer() for javascript {
    return $theAnswer()
}

void test_invoke_quark_function_from_native() {
    assertEqual(theAnswer(), getTheAnswer());
}

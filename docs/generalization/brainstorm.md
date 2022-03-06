# Constraint Language Definition (CLD)

`Last update: 20/2/22`

Core requirements:
- humanly readable
- as concise as possible
- easy & efficient implementation
- not trying to solve everything

To satisfy humanly readable, follow basic english Syntax: `Subject -> Verb -> Object`.
To simplify this, subject is always the **Agent**. 
example:
```
Agent blocked on Action at time > 3 
```


## Basic keywords

### Agent

```bash
# rule applicable to all agents
"Agent ..." 

# rule applicable to specific agent(s) (0 & 1)
"Agent '0', '1'..."
```

### Action (ACTION)
needs to have a reference to:
- agent 
- traversing edge, i.e. origin (orig) & destination (dest)
- time (t)
- name

## Applicability (Precondition)
For now, this is not supported, as it would require moddeling actions.
Moreover, this can be done by PDDL as well

## Conflicts (Postcondition)
This is where the interest lies, as in case of decentralised solvers such as CBS, 
multiple applicable actions are validated agains each other.


# Brainstorming

Previous work:
Vertex constrait: <a,v,t> -> { (a, e_i, t-1), (...), ... }, e.head = v
Edge constraint:  <a,v1,v2,t> -> { <a,e,t,> }, e = (v1,v2)


Examples of rules (proposed by Kasper):
$$
  R_{edge} = <e, {e'}>,  e \in E  \setminus{E_{wait}}                            \\
  R_{vertex} = <e, E_v>, e \in E, e.head = v                                     \\
  R_{dist(x)} = <e, E_{v,x}>, e \in E,                                           \\ 
  R_{area} = <e, E_{area}>, e \in E_{area},                                      \\
  R_{corridor} = <e, P'>, e \in P \cup <e, P>
$$

The advantage of previous work is that it's generic enough (although has its limitations).

```markdown
Basic Syntax: ACTION a BLOCKED BY ACTION b IF [SOME PREDICATE USING a and b]
Predicates could be complex using OR|AND|NOT

# Agent blocked by Other when their destination overlaps:
- Rule: R{vertex} 
- Example 1: ACTION a BLOCKED BY ACTION b IF a.dest IS b.dest
- Example 2: ACTION a BLOCKED BY ACTION b IF a.destination IS b.destination

# Edge blocked if traversed by two agents simultaneously
- Rule: R{edge}
- Example 1: ACTION a BLOCKED BY ACTION b IF a.edge OVERLAPS WITH b.edge

# Agent blocked by Other if their destination is NOT/LESS/MORE
- Rule: R{dest}
- Example 1: ACTION a BLOCKED BY ACTION b IF DISTANCE(a.orig, b.orig) IS [[LESS, MORE] THAN] x
- Example 2: ACTION a BLOCKED BY ACTION b IF DISTANCE(a.orig, b.orig) IS LESS THAN x
- Example 3: ACTION a BLOCKED BY ACTION b IF DISTANCE(a.orig, b.orig) IS x
- Example 4: ACTION a BLOCKED BY ACTION b IF DISTANCE(a.orig, b.orig) IS NOT x

# Blocking in Areas
- Rule: R{area}
- Example 1: ACTION a BLOCKED BY ACTION b if AREA(a.edge) OVERLAPS WITH AREA(b.edge)

# Blocking Corridors
- Rule: R{corridor}
Not sure how to do this yet in a clean way

# A very generic approach, that could work for any kind of rule, but would need more implementation, 
- Rule: R{func}
- Syntax: FUNC('FUNCTION_NAME', arg1, arg2, ...) -> boolean
- Example 1: ACTION a BLOCKED BY ACTION b if FUNC('goingThroughCorridor', a.edge, b.edge)
- Requirement: implement a function 'goingThroughCorridor' that takes two edges as arguments
```
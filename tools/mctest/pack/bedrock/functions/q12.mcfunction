# [Q12] quantity 取反：正好3应不命中 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 3
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q12s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=3}] run scoreboard players set #q12s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=!3}] run scoreboard players set #q12 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5,quantity=!3}] run scoreboard players set #q12 tt 0

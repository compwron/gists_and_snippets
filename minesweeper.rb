grid_x = 3
grid_y = 4

class GridBlock
  attr_reader :revealed, :flag, :nearby_flags

  def initialize(revealed:, flag:, nearby_flags:)
    @revealed = revealed
    @flag = flag
    @nearby_flags = nearby_flags
  end

  def player_view
    if revealed
      if flag
        red('F')
      else
        nearby_flags
      end
    else
      '?'
    end
  end

  def red(data)
    "\e[31m#{data}\e[0m"
  end

  def set_revealed_flag!
    @revealed = true
    @flag = true
  end

  def reveal!
    @revealed = true
  end

  def to_s
    {
      revealed: revealed,
      flag: flag,
      nearby_flags: nearby_flags,
    }
  end
end

grid = Array.new(grid_y) { |_| Array.new(grid_x) { |_| GridBlock.new(revealed: false, flag: false, nearby_flags: nil) } }

# grid[0][0].set_revealed_flag!
grid[0][1].set_revealed_flag!


def print_grid(grid)
  grid.map { |x| x.map { |y| y.player_view }.join }.reverse
end

puts print_grid(grid)
